
// const {OAuth2Client} = require('google-auth-library');

import verifyToken from "../auth";

import { v4 as uuid } from 'uuid';

// const GOOGLE_CLIENT_ID = process.env.VITE_GOOGLE_CLIENT_ID
// const client = new OAuth2Client();

const AWS = require('aws-sdk');

// Set the region where your DynamoDB table is located
AWS.config.update({ region: 'us-west-1' });

// Create a new DynamoDB client
const dynamodb = new AWS.DynamoDB.DocumentClient();

export const getUserData = async (auth: string) => {
  try {
    //console.log("token " + auth)
    //console.log("try to auth user")
    const token = auth.split(' ')[1];
    
    //console.log("token : " + token)
    const ticket = await verifyToken(token);

  if(!ticket) {
    console.log("якогось * пустий auth від гугла")
  }
  const email = ticket?.email;
  const userId = ticket?.sub;
  console.log(email);

  //console.log("logging")
  const params = {
      TableName: 'users',
      Key: {
        email: email
      }
    };

    //console.log(params)

  const dbData = await dynamodb.get(params).promise();

  //console.log(dbData)

  if (Object.keys(dbData).length === 0) {

    console.log("try to init user")
    const saveParams = {
      TableName: 'users',
      Item: {
        email: email,
        userId: userId, 
        last_activity: new Date().toISOString(),
        auth: "google"
      }
    };

    //console.log(saveParams)

    await dynamodb.put(saveParams).promise();
    await default_chats(email);

  } else {
    console.log("try to update user")
    const updateParams = {
      TableName: 'users',
      Key: {
        email: email
      },
      UpdateExpression: 'set #attrName = :attrValue',
      ExpressionAttributeNames: {
        '#attrName': 'last_activity'
      },
      ExpressionAttributeValues: {
        ':attrValue': new Date().toISOString(),
      },
      ReturnValues: 'UPDATED_NEW'
    };

    console.log(updateParams)

    await dynamodb.update(updateParams).promise();

  }

    // console.log(ticket)
    return ticket;
  } catch (error: any) {
    console.log(error)
    return null;
  }
};


async function default_chats(email: string){

  const onboarding = {
    TableName: 'chats',
    Item: {
      'chatId':  uuid(),
      'description': 'Onboarding',
      'isVisible':  true,
      'email': email,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };

  //console.log(onboarding)

 await dynamodb.put(onboarding).promise();


  const conflict = {
    TableName: 'chats',
    Item: {
      'chatId':  uuid(),
      'description': 'Conflict resolution',
      'isVisible':  false,
      'email': email,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };

  await dynamodb.put(conflict).promise();


  const communication = {
    TableName: 'chats',
    Item: {
      'chatId':  uuid().toString(),
      'description': 'Communication',
      'isVisible':  false,
      'email': email,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };

  await dynamodb.put(communication).promise();


  const leadership = {
    TableName: 'chats',
    Item: {
      'chatId': uuid().toString(),
      'description': 'Leadership',
      'isVisible':  false,
      'email': email,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };
  await dynamodb.put(leadership).promise();

  const adaptability = {
    TableName: 'chats',
    Item: {
      'chatId':  uuid().toString(),
      'description': 'Adaptability',
      'isVisible':  false,
      'email': email,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };

  await dynamodb.put(adaptability).promise();


  const problem_solving = {
    TableName: 'chats',
    Item: {
      'chatId':  uuid().toString(),
      'description': 'Problem-Solving',
      'email': email,
      'isVisible':  false,
      'createdAt':  Date.now(),
      'score':  0 ,
    }
  };

  await dynamodb.put(problem_solving).promise();

  console.log("created default chats for u")
}