<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
  <title>TTY Community</title>

</head>
<body>
<p>Welcome to TTY-Community Server Page!</p>

<form action="api/user/change_portrait" method="post" enctype="multipart/form-data">
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
  <p class="leftDiv">上传头像</p>
  <p class="rightDiv">
    <input type="file" name="portrait" class="text">
  </p>
  <br>
  <p class="rightDiv">
    <input type="submit" value="上传" class="button">
  </p>
</form>
</body>
</html>