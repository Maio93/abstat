#!/bin/bash

relative_path=`dirname $0`
root=`cd $relative_path;pwd`

cd $root/../summarization

#Setto le variabili
JAVA_HOME="/usr" #Server: /usr/lib/jvm/java-6-sun
SPARK_HOME="$SPARK"
HADOOP_HOME="$HADOOP"
debug=1 #0: Disabled, 1:Enabled
#Setto opportunamente il comando di debug
if [ $debug -eq 1 ]
then
	dbgCmd="/usr/bin/time -f \"COMMAND: %C\nTIME: %E real\t%U user\t%S sys\nCPU: %P Percentage of the CPU that this job got\nMEMORY: %M maximum resident set size in kilobytes\n\n\" "
else
	dbgCmd=""
fi

DataDirectory=$1
ResultsDirectory=$2
AwkScriptsDirectory=awk-scripts
TripleFile=dataset.nt

#Variabili per il calcolo del report dell'ontologia
OntologyFile="$DataDirectory/ontology/"
ReportDirectory="$ResultsDirectory/reports/"
TmpDatasetFileResult="$ResultsDirectory/reports/tmp-data-for-computation/"

#Variabili per il calcolo del report del dataset
DatasetFile="$DataDirectory/triples"
tmpDatasetFile="$DataDirectory/organized-splitted-deduplicated-tmp-file"
orgDatasetFile="$DataDirectory/organized-splitted-deduplicated"

#MinType
minTypeResult="$ResultsDirectory/min-types/min-type-results"

log_file="../data/logs/summarization/log.txt"

#Lettere con cui splitto i file per la parallelizzazione
IFS=',' read -a splitters <<< "0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,g,h,i,j,k,l,m,n,o,p,q,r,s,t,u,v,w,x,y,z,%,_,others" #Allineare con quanto presente in organize:data, se modifico
NProc=4 #Numero di processi da parallelizzare [I passi successivi sono, per ora, vincolati all'uso di 4 processori]
NUM=0
QUEUE=""
#Utilizzate per calcolare il tempo di esecuzione totale
start=$SECONDS

#Funzioni utili alla gestione delle code di esecuzione di processi
function queue {
	QUEUE="$QUEUE $1"
	NUM=$(($NUM+1))
}

function regeneratequeue {
	OLDREQUEUE=$QUEUE
	QUEUE=""
	NUM=0
	for PID in $OLDREQUEUE
	do
		if [ -d /proc/$PID  ] ; then
			QUEUE="$QUEUE $PID"
			NUM=$(($NUM+1))
		fi
	done
}

function checkqueue {
	OLDCHQUEUE=$QUEUE
	for PID in $OLDCHQUEUE
	do
		if [ ! -d /proc/$PID ] ; then
			regeneratequeue # at least one PID has finished
			break
		fi
	done
}

rm -rf ${TmpDatasetFileResult}* #Rimuovo tutti i file Tmp_Data_For_Computation
mkdir -p $TmpDatasetFileResult

rm -f $log_file
touch $log_file

{ 
echo "---Start: Ontology Report---"

	export OntologyFile
	export ReportDirectory
	export TmpDatasetFileResult

	eval ${dbgCmd}""$JAVA_HOME/bin/java -Xms256m -Xmx4000m -cp summarization.jar it.unimib.disco.summarization.export.ProcessOntology "$OntologyFile" "$TmpDatasetFileResult"
	if [ $? -ne 0 ]
	then
	    echo "App Failed during run"
	    exit 1
	fi

echo "---End: Ontology Report---"

echo ""

} &>> $log_file

{ 
	echo "---Start: Organize and Split files---"

	startBlock=$SECONDS

	#Divido i file da organizzare in NProc parti uguali l'uno così da parallelizzare l'organizzazione
	rm -rf $tmpDatasetFile #Rimuovo la directory che conterrà i file temporanei
	mkdir -p $tmpDatasetFile #Creo la directory che conterrà i file temporanei

	#Calcolo la dimensione dei file da splittare
	dataSize1=$(stat --printf="%s" $DatasetFile/$TripleFile) 
 
	let "dataBlockSize1=($dataSize1/$NProc)+10000000" #Aggiungo 10000000 così da assicurarmi di salvare tutte le informazioni nel passo successivo

	#Processo da eseguire per lo splittaggio
	splitFile="split -u -C $dataBlockSize1 $DatasetFile/$TripleFile $tmpDatasetFile/1_lod_part_" #-u per scrittura diretta senza bufferizzazione

	#Splitto il file utilizzando dataBlockSize
	eval ${dbgCmd}""${splitFile}
	sync #Mi assicuro che tutte le informazioni siano scritte su file

	#Creo le stringhe contenenti i file da organizzare
	filePartCount=0
	stringFile[0]="aa"
	stringFile[1]="ab"
	stringFile[2]="ac"
	stringFile[3]="ad"
	for (( i=0; i<${#stringFile[@]}; i++ ))
	do
		filePart=""
		if [ -f $tmpDatasetFile/1_lod_part_${stringFile[$i]} ];
		then
			if [ filePart == "" ]
			then
				filePart="$tmpDatasetFile/1_lod_part_${stringFile[$i]}"
			else
				filePart="${filePart} $tmpDatasetFile/1_lod_part_${stringFile[$i]}"
			fi
		fi
		filePartCom[$filePartCount]=${filePart}
		filePartCount=$(($filePartCount+1))	

	done

	rm -f $orgDatasetFile/*.nt  2>/dev/null #Rimuovo i file generati nell'esecuzione precedente
	mkdir -p $orgDatasetFile

	#Processi da eseguire per l'organizzazione (Assumo che le stringhe di file abbiano almeno un file)
	orgFile[0]="gawk -f $AwkScriptsDirectory/organize_data.awk -v prefix=1 -v destinatioDirectory=\"${orgDatasetFile}\" ${filePartCom[0]}"
	orgFile[1]="gawk -f $AwkScriptsDirectory/organize_data.awk -v prefix=2 -v destinatioDirectory=\"${orgDatasetFile}\" ${filePartCom[1]}"
	orgFile[2]="gawk -f $AwkScriptsDirectory/organize_data.awk -v prefix=3 -v destinatioDirectory=\"${orgDatasetFile}\" ${filePartCom[2]}"
	orgFile[3]="gawk -f $AwkScriptsDirectory/organize_data.awk -v prefix=4 -v destinatioDirectory=\"${orgDatasetFile}\" ${filePartCom[3]}"

	#Rinizializzo le variabili della parallelizzazione, per sicurezza
	NUM=0
	QUEUE=""

	#Avvio l'esecuzione parallela dei processi
	for (( proc=0; proc<${#orgFile[@]}; proc++ )) # for the rest of the arguments
	do
		#echo ${orgFile[$proc]}
		eval ${dbgCmd}""${orgFile[$proc]} &
		PID=$!
		queue $PID

		while [ $NUM -ge $NProc ]; do
			checkqueue
			sleep 0.4
		done
	done
	wait # attendi il completamento di tutti i processi prima di procedere con il passo successivo
	sync #Mi assicuro che tutte le informazioni siano scritte su file

	#Rimuovo le singole parti (Separato perchè in parallelo si crea dipendenza tra coppie di processi, ed essendo la rimozione veloce si può gestire senza problemi così)
	rm -f $tmpDatasetFile/1_lod_part_aa
	rm -f $tmpDatasetFile/1_lod_part_ab
	rm -f $tmpDatasetFile/1_lod_part_ac
	rm -f $tmpDatasetFile/1_lod_part_ad
	
	rm -rf $tmpDatasetFile/ #Rimuovo la directory con i file temporanei, non più utili

	endBlock=$SECONDS
	if [ $debug -eq 1 ]
	then
		echo "Time: $((endBlock - startBlock)) secs."
		echo ""
	fi

	echo "---End: Organize and Split files---"
	echo ""
	echo "---Start: Deduplication of files---"
	
	startBlock=$SECONDS
	
	#Creo i processi che andranno ad unire i file (2>/dev/null per non scrivere errori di file non esistenti, perchè il cat funziona comunque su tutti quelli che ci sono)
	numMerge=0
	for element in "${splitters[@]}"
	do
	   #TODO: Per generalizzare, bisogna verificare se i file ci sono e creare dinamicamente il comando perchè cat crea il file vuoto anche se i file sorgente non vi sono
	   #Unisco i file types
	   mergeFile[$numMerge]="cat ${orgDatasetFile}/1${element}_types.nt ${orgDatasetFile}/2${element}_types.nt ${orgDatasetFile}/3${element}_types.nt ${orgDatasetFile}/4${element}_types.nt > ${orgDatasetFile}/${element}_types.nt 2>/dev/null"
	   numMerge=$(($numMerge+1))
	   #Unisco i file obj_properties
	   mergeFile[$numMerge]="cat ${orgDatasetFile}/1${element}_obj_properties.nt ${orgDatasetFile}/2${element}_obj_properties.nt ${orgDatasetFile}/3${element}_obj_properties.nt ${orgDatasetFile}/4${element}_obj_properties.nt > ${orgDatasetFile}/${element}_obj_properties.nt 2>/dev/null"
	   numMerge=$(($numMerge+1))
	   #Unisco i file dt_properties
	   mergeFile[$numMerge]="cat ${orgDatasetFile}/1${element}_dt_properties.nt ${orgDatasetFile}/2${element}_dt_properties.nt ${orgDatasetFile}/3${element}_dt_properties.nt ${orgDatasetFile}/4${element}_dt_properties.nt > ${orgDatasetFile}/${element}_dt_properties.nt 2>/dev/null"
	   numMerge=$(($numMerge+1))
	done

	#Unisco i file
	#Rinizializzo le variabili della parallelizzazione, per sicurezza
	NUM=0
	QUEUE=""

	#Avvio l'esecuzione parallela dei processi
	for (( proc=0; proc<${#mergeFile[@]}; proc++ )) # for the rest of the arguments
	do
		#echo ${mergeFile[$proc]}
		eval ${dbgCmd}""${mergeFile[$proc]} &
		PID=$!
		queue $PID

		while [ $NUM -ge $NProc ]; do
			checkqueue
			sleep 0.4
		done
	done
	wait # attendi il completamento di tutti i processi prima di procedere con il passo successivo
	sync #Mi assicuro che tutte le informazioni siano scritte su file

	#Rimuovo le singole parti (Separato perchè in parallelo si crea dipendenza tra coppie di processi, ed essendo la rimozione veloce si può gestire senza problemi così)
	for i in 1 2 3 4
	do
		for element in "${splitters[@]}"
		do
		   #Elimino i file types
		   rm -f ${orgDatasetFile}/${i}${element}"_types.nt"
		   #Elimino i file obj_properties
		   rm -f ${orgDatasetFile}/${i}${element}"_obj_properties.nt"
		   #Elimino i file dt_properties
		   rm -f ${orgDatasetFile}/${i}${element}"_dt_properties.nt"
		done
	done

	#Creo i processi che andranno a rimuovere i duplicati
	numDedupl=0
	for element in "${splitters[@]}"
	do
	   #Elimino i duplicati dai file types
	   if [ -f ${orgDatasetFile}/${element}_types.nt ];
	   then
		   deduplFile[$numDedupl]="sort -u ${orgDatasetFile}/${element}_types.nt -o ${orgDatasetFile}/${element}_types.nt"
		   numDedupl=$(($numDedupl+1))
	   fi
	   #Elimino i duplicati dai file obj_properties
	   if [ -f ${orgDatasetFile}/${element}_obj_properties.nt ];
	   then
		   deduplFile[$numDedupl]="sort -u ${orgDatasetFile}/${element}_obj_properties.nt -o ${orgDatasetFile}/${element}_obj_properties.nt"
		   numDedupl=$(($numDedupl+1))
	   fi
	   #Elimino i duplicati dai file dt_properties
	   if [ -f ${orgDatasetFile}/${element}_dt_properties.nt ];
	   then
		   deduplFile[$numDedupl]="sort -u ${orgDatasetFile}/${element}_dt_properties.nt -o ${orgDatasetFile}/${element}_dt_properties.nt"
		   numDedupl=$(($numDedupl+1))
	   fi
	done

	#Rinizializzo le variabili della parallelizzazione, per sicurezza
	NUM=0
	QUEUE=""

	#Avvio l'esecuzione parallela dei processi
	for (( proc=0; proc<${#deduplFile[@]}; proc++ )) # for the rest of the arguments
	do
		#echo ${deduplFile[$proc]}
		eval ${dbgCmd}""${deduplFile[$proc]} &
		PID=$!
		queue $PID

		while [ $NUM -ge $NProc ]; do
			checkqueue
			sleep 0.4
		done
	done
	wait # attendi il completamento di tutti i processi prima di procedere con il passo successivo
	sync #Mi assicuro che tutte le informazioni siano scritte su file

	endBlock=$SECONDS
	if [ $debug -eq 1 ]
	then
		echo "Time: $((endBlock - startBlock)) secs."
		echo ""
	fi

	echo "---End: Deduplication of files---"
	echo ""

} &>> $log_file

{
echo "---Start: Upload data---"
	startBlock=$SECONDS

	master='local[4]'

	if [ "$master" == "yarn --deploy-mode cluster" ]
	then
		#carico dati su hdfs
		$HADOOP_HOME/bin/hdfs dfs -rm -R "hdfs://master:54310/home"
		$HADOOP_HOME/bin/hdfs dfs -mkdir -p "hdfs://master:54310/home/fabio/Scrivania/Stage/abstat/data"

		$HADOOP_HOME/bin/hdfs dfs -put "/home/fabio/Scrivania/Stage/abstat/data/datasets" "hdfs://master:54310/home/fabio/Scrivania/Stage/abstat/data"
		$HADOOP_HOME/bin/hdfs dfs -put "$SPARK_HOME/lib/spark-assembly-1.6.1-hadoop2.6.0.jar" "/home"
		$HADOOP_HOME/bin/hdfs dfs -put "summarization.jar" "/home"
		JAR="hdfs://master:54310/home/summarization.jar"
		CONF="--conf spark.yarn.jar=\"hdfs://master:54310/home/spark-assembly-1.6.1-hadoop2.6.0.jar\" "
	else
		JAR="summarization.jar"
		CONF=""
	fi

	endBlock=$SECONDS
	if [ $debug -eq 1 ]
	then
		echo "Time: $((endBlock - startBlock)) secs."
		echo ""
	fi

	echo "---End: Upload Data---"
	echo ""
} &>> $log_file

{ 
echo "---Start: Counting---"
	startBlock=$SECONDS	

	rm -rf $minTypeResult
	mkdir -p $minTypeResult

	rm -rf $ResultsDirectory/patterns
	mkdir -p $ResultsDirectory/patterns
	
	eval ${dbgCmd}""$SPARK_HOME/bin/spark-submit --master $master --name "summarization_part1" --class it.unimib.disco.summarization.export.CalculateMinimalTypes $CONF $JAR "$OntologyFile" "$orgDatasetFile" "$minTypeResult"
	if [ $? -ne 0 ]
	then
	    echo "App Failed during run"
	    exit 1
	fi

	if [ "$master" == "yarn --deploy-mode cluster" ]
	then
		$HADOOP_HOME/bin/hdfs dfs -put "/home/fabio/Scrivania/Stage/abstat/data/summaries" "hdfs://master:54310/home/fabio/Scrivania/Stage/abstat/data"
	fi

	eval ${dbgCmd}""$SPARK_HOME/bin/spark-submit --master $master --name "summarization_part2" --class it.unimib.disco.summarization.export.AggregateConceptCounts $CONF $JAR "$minTypeResult" "$ResultsDirectory/patterns/"

	if [ $? -ne 0 ]
	then
	    echo "App Failed during run"
	    exit 1
	fi

	eval ${dbgCmd}""$SPARK_HOME/bin/spark-submit --master $master --name "summarization_part3" --class it.unimib.disco.summarization.export.ProcessDatatypeRelationAssertions $CONF $JAR "${orgDatasetFile}" "$minTypeResult" "$ResultsDirectory/patterns/"

	if [ $? -ne 0 ]
	then
	    echo "App Failed during run"
	    exit 1
	fi

	eval ${dbgCmd}""$SPARK_HOME/bin/spark-submit --master $master --name "summarization_part4" --class it.unimib.disco.summarization.export.ProcessObjectRelationAssertions $CONF $JAR "${orgDatasetFile}" "$minTypeResult" "$ResultsDirectory/patterns/"

	if [ $? -ne 0 ]
	then
	    echo "App Failed during run"
	    exit 1
	fi

	endBlock=$SECONDS
	if [ $debug -eq 1 ]
	then
		echo "Time: $((endBlock - startBlock)) secs."
		echo ""
	fi

	echo "---End: Counting---"

	echo ""
} &>> $log_file
{ 
	startBlock=$SECONDS
	echo "---Start: Cleaning---"
	rm -f ${orgDatasetFile}/*_types.nt
	rm -f ${minTypeResult}/*_uknHierConcept.txt
	endBlock=$SECONDS
	
	if [ $debug -eq 1 ]
	then
		echo "Time: $((endBlock - startBlock)) secs."
		echo ""
	fi

	echo "---End: Cleaning---"
	echo ""

} &>> $log_file

{
	end=$SECONDS
	if [ $debug -eq 1 ]
	then
		echo "Total Time: $((end - start)) secs."
		echo ""
	fi

} &>> $log_file

