const express = require('express');
import { Request, Response, Router } from 'express';

const router: Router = express.Router();

const AWS = require('aws-sdk');

AWS.config.update({ region: 'us-west-1' });

const dynamodb = new AWS.DynamoDB.DocumentClient();

router.get('/get', async (req: Request, res: Response) => {
  console.log("get request messages")
  const { chatId } = req.query;
  if(!chatId) {
    res.send({ messages: [] });
  }
  const params = {
    TableName: 'messages',
    FilterExpression: 'chatId = :chatId',
    ExpressionAttributeValues: {
      ':chatId': chatId
    }
  };

  console.log(params)
try{
const messages = await dynamodb.scan(params).promise();

console.log(messages.Items)
res.send({ messages: messages.Items?? [] });
  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
  }
});

router.post('/save', async (req: Request, res: Response) => {
  
  console.log("save messages request")
  const message = req.body.message;
  
  console.log(message)

  const params = {
    TableName: 'messages',
    Item: message
    }

try{
dynamodb.put(params).promise();
// Send the base64 image as a response  
res.send({ success: true });

  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
  }
});

export default router;
