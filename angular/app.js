var express = require('express');
var app = express();
var port = process.env.PORT || 8087;

app.disable('x-powered-by');
app.use(express.compress());

app.get('*', function(req, res) {
  return res.sendfile(__dirname + '/app.html');
});

app.listen(port, function() {
  console.log('server up on %d', port);
});
