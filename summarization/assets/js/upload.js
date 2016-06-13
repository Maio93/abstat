$("#fileUpload").submit(function(e) {
  submitForm(this);
  e.preventDefault();
});

function submitForm(form) {
  var url = $(form).attr("action");
  var formData = {};
  formData.filesInfo = [];
  var files = $('#dataset').prop("file");

  $(files).each(function() {
    var fileInfo = {};
    fileInfo.name = this.name;
    fileInfo.size = this.size;
    fileInfo.type = this.type;
    formData.filesInfo.push(fileInfo);
  });

  $.post(url, formData).done(function (data) {
    alert(data);
  });
}
