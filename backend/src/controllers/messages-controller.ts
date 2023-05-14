import {getNextMessage} from "../repository/QuestionRepository";

const express = require('express');
import {Request, Response, Router} from 'express';
import {ChatType} from "../entity/ChatType";

const router: Router = express.Router();

const AWS = require('aws-sdk');

AWS.config.update({region: 'us-west-1'});

const dynamodb = new AWS.DynamoDB.DocumentClient();

router.get('/get', async (req: Request, res: Response) => {

  console.log("get request messages")
  const { chatId } = req.query;
  if(!chatId) {
    res.send({ messages: [] });
  }
    console.log(chatId)
    const params = {
        TableName: 'messages',
        FilterExpression: 'chatId = :chatId',
        ExpressionAttributeValues: {
            ':chatId': chatId
        }
    };

    try {
        const messages = dynamodb.scan(params).promise();
// Send the base64 image as a response
        if (isNotLastQuestion(chatId)) {
            getNextMessage(getUserId(chatId), getChatType(chatId), "lastAnswer")
        } else {
            res.send({messages: messages.Items ?? []});
        }

    } catch (error: any) {
        console.error(error);
        console.log(error.response.data)
    }
});


function isNotLastQuestion(chatIt: string): Boolean {
    return true//TODO: Not implement
}

function getChatType(chatIt: string): ChatType {
    return //TODO: Not implement
}

function getUserId(chatIt: string): String {
    return ""//TODO: Not implement
}

router.post('/save', async (req: Request, res: Response) => {
  
  console.log("save messages request")
  const message = req.body.message;

    console.log(message)

    const params = {
        TableName: 'messages',
        Item: message
    }

    try {
        dynamodb.put(params).promise();
// Send the base64 image as a response  
        res.send({success: true});

    } catch (error: any) {
        console.error(error);
        console.log(error.response.data)
    }
});

export default router;
