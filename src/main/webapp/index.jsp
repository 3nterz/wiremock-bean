<html>
<body>
<h2>Wiremock Mock Server Bean Homepage</h2>
<jsp:useBean id="wiremockMockServer" class="dev.nathan.wiremock.app.WiremockMockServer" scope="application"/>
<div>Wiremock URL: http://localhost:<jsp:getProperty name="wiremockMockServer" property="port" /></div>
</body>
</html>
