const express = require('express')
const router = express.Router();
const db = require('../model/db')

router.get('/', (request, response) => {
    response.json({ info: 'Node.js, Express, and Neo4J API' })
  })


router.post('/command', db.createCommand);
router.post('/illumination', db.updateIllumination);


module.exports = router;