const createError = require('http-errors');
const express = require('express')
const bodyParser = require('body-parser')
const indexRouter = require('./routes/index');
const cors = require('cors');




const app = express()

app.use(cors({
  origin: '*'
}));


app.use(bodyParser.json());
app.use(
    bodyParser.urlencoded({
      extended: true,
    })
  )

//Content type for all the requests
app.use(function(req, res, next) {
    res.setHeader('Content-Type', 'application/ld+json');
    next();
  });

app.use('/', indexRouter);

// catch 404 and forward to error handler
app.use(function(req, res, next) {
    next(createError(404));
  });
  
  // error handler
  // eslint-disable-next-line no-unused-vars
  app.use(function(err, req, res, next) {
    // set locals, only providing error in development
    res.locals.message = err.message;
    res.locals.error = process.env.NODE_ENV === 'development' ? err : {};
    
    // render the error page
    res.status(err.status || 500);
    res.json({ error: err.message })
  });


  module.exports = app;