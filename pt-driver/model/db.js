const neo4j = require('neo4j-driver');

 /*const driver = neo4j.driver(
  "neo4j://<host>:<port>",
  neo4j.auth.basic("<username>", "<password>")
);*/


const createCommand = (request, response) => {
    const session = driver.session();
    var { commandOperation, twinClass, twinId } = request.body

    session
     .run("CREATE (Command1:Command{commandOperation:$commandOperation, twinClass: $twinClass, twinId:$twinId, executionId:$unixTime, isProcessed:false})", {
      commandOperation: commandOperation,
      twinClass: twinClass,
      twinId: twinId,
      unixTime: Math.floor(Date.now() / 1000)
     })
     .then(result => {
       session.close();
     });

  }

const updateIllumination = (request, response) => {
  const session = driver.session();
  var { officeNumber, value } = request.body
  console.log(request.body);

  session
   .run("CREATE (Illumination" + officeNumber + ":Illumination{name:$roomName, value:$illuminationValue, executionId:$unixTime, isProcessed:false})", {
    roomName: 'office' + officeNumber,
    illuminationValue: value,
    unixTime: Math.floor(Date.now() / 1000)
    })
    .then(result => {
      session.close();
    });
 }


  module.exports = {
    createCommand,
    updateIllumination
  }