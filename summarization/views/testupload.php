<?php

define(“UPLOAD_DIR”, “/home/fabio/Scrivania/Stage/Fork_abstat/abstat/data/datasets/”);

if(isset($_POST[‘action’]) and $_POST[‘action’] == ‘upload’)
{
    if(isset($_FILES[‘dataset’]))
    {
        $file = $_FILES[‘dataset’];
        if($file[‘error’] == UPLOAD_ERR_OK and is_uploaded_file($file[‘tmp_name’]))
        {
            move_uploaded_file($file[‘tmp_name’], UPLOAD_DIR.$file[‘name’]);
        }
    }
}

?>