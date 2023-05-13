const express = require('express');
import { Request, Response, Router } from 'express';
import verifyToken from '../auth';

const router: Router = express.Router();

const AWS = require('aws-sdk');

AWS.config.update({ region: 'us-west-1' });

const dynamodb = new AWS.DynamoDB.DocumentClient();

router.get('/getall', async (req: Request, res: Response) => {

  console.log("getall chats request")
   
  const auth = req.headers.authorization;

  const authToken = auth?.split(" ")[1];

  //console.log(authToken)
 
  const ticket = await verifyToken(authToken+"");
  const email = ticket.email;

  //console.log(email)
  if(!email) {
    console.log("все пропало auth без email")
    res.send({ chats: []});
  }

 const params = {
  TableName: 'chats', 
  FilterExpression: 'email = :email',
  ExpressionAttributeValues: {
    ':email': email
  }

};

//console.log(params)

try{
const chats_response = await dynamodb.scan(params).promise();

//console.log(chats_response)
// Send the base64 image as a response  
res.send({ chats: chats_response.Items ?? []});

  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
  }
});

router.get('/get', async (req: Request, res: Response) => {
  console.log("get chat request")

  const auth = req.headers.authorization;
  const authToken = auth?.split(" ")[1];
  //console.log(authToken)
 
  const ticket = await verifyToken(authToken + "");
  const email = ticket.email;

  if(!email) {
    console.log("все пропало auth без email")
    res.send({ chat: {} });
  }

  const { chatId } = req.query;


  if(!chatId) {
    res.send({ chat: {} });
  }
  console.log(chatId)
  console.log(req)
  const params = {
    TableName: 'chats',
    Key: {
      'chatId': chatId
    }
  };

try{
const chat = await dynamodb.get(params).promise(); 
console.log(chat)
res.send({ chat: chat.Item?? {} });

  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
  }
});

router.post('/save', async (req: Request, res: Response) => {

  console.log("save chats request")

  const auth = req.headers.authorization;
  const authToken = auth?.split(" ")[1];

  const chat = req.body.chat;

  //console.log(authToken+"")
  const ticket = await verifyToken(authToken+"");
  //console.log(ticket)
  const email = ticket.email;
 

  if(!email) {
    console.log("все пропало auth без email")
    res.send({ chat: {} });
  }

  chat.email = email;
  
  //console.log(chat)

  const params = {
    TableName: 'chats',
    Item: chat
    }

try{
dynamodb.put(params).promise();
res.send({ success: true });

  } catch (error: any) {
    console.error(error);
    console.log(error.response.data)
  
    // if (
    //   error.response &&
    //   error.response.data &&
    //   error.response.data.message &&
    //   error.response.data.message.includes('Missing image file')
    // ) {
    //   res.status(400).send({ message: 'Image file is missing from the request' });
    // } else {
    //   res.status(500).send({ message: 'Internal Server Error' });
    // }
  }
});

export default router;
