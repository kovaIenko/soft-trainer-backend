
// const {OAuth2Client} = require('google-auth-library');

import verifyToken from "../auth";

// const GOOGLE_CLIENT_ID = process.env.VITE_GOOGLE_CLIENT_ID
// const client = new OAuth2Client();

const AWS = require('aws-sdk');

// Set the region where your DynamoDB table is located
AWS.config.update({ region: 'us-west-1' });

// Create a new DynamoDB client
const dynamodb = new AWS.DynamoDB.DocumentClient();

export const getUserData = async (auth: string) => {
  try {

    console.log("try to auth user")
    const token = auth.split(' ')[1];
    
    //console.log("token : " + token)
    const ticket = await verifyToken(token);

  if(!ticket) {
    console.log("якогось хуя пустий auth від гугла")
  }
  const email = ticket?.email;
  const userId = ticket?.sub;
  console.log(email);

  console.log("logging")
  const params = {
      TableName: 'users',
      Key: {
        email: email
      }
    };

  const dbData = await dynamodb.get(params).promise();

  console.log("dbData")
  console.log(dbData)

  if (!dbData) {

    const saveParams = {
      TableName: 'users',
      Key: {
        email: email,
        userId: userId, 
        last_activity: new Date().toISOString(),
        auth: "google"
      }
    };
    dynamodb.put(saveParams).promise();
  } else {
  
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

    await dynamodb.update(updateParams).promise();
  }

    // console.log(ticket)
    return ticket;
  } catch (error: any) {
    console.log(error)
    return null;
  }
};
