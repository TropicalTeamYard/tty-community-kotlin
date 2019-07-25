<%@ page contentType="text/html;charset=GBK" language="java" %>
<html>
<head>
  <title>TTY Community</title>

</head>
<body>
<p>Welcome to TTY-Community Server Page!</p>

<form action="api/blog/create" method="post" enctype="multipart/form-data">
  <p class="leftDiv">上传文件一</p>
  <p class="rightDiv">
    <input type="file" name="file1" class="text">
  </p>
  <p class="leftDiv">上传文件二</p>
  <p class="rightDiv">
    <input type="file" name="file2" class="text">
  </p>
  <label>
    ID
    <input name="id" type="text">
  </label>
  <br>
  <label>
    TOKEN
    <input name="token" type="text">
  </label>
  <br>
  <label>
    TYPE
    <input name="type" type="text">
  </label>
  <br>
  <label>
    TITLE
    <input name="title" type="text">
  </label>
  <br>
  <label>
    INTRODUCTION
    <input name="introduction" type="text">
  </label>
  <br>
  <label>
    CONTENT
    <input name="content" type="text">
  </label>
  <br>
  <label>
    TAG
    <input name="tag" type="text">
  </label>
  <br>
  <label>
    FILE_COUNT
    <input name="file_count" type="text">
  </label>
  <br>
  <p class="rightDiv">
    <input type="submit" value="上传" class="button">
  </p>
</form>
</body>
</html>