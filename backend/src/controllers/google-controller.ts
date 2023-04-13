import axios from 'axios';

const AWS = require('aws-sdk');

// Set the region where your DynamoDB table is located
AWS.config.update({ region: 'us-west-1' });

// Create a new DynamoDB client
const dynamodb = new AWS.DynamoDB.DocumentClient();


export const getUserData = async (auth: string) => {
  try {
    const token = auth.split(' ')[1];
    const { data } = await axios.get(
      'https://www.googleapis.com/oauth2/v3/userinfo',
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      },
    );

  console.log("logging")
  const params = {
      TableName: 'users',
      Key: {
        email: data.email
      }
    };

  const dbData = await dynamodb.get(params).promise();

  console.log(dbData)

  if (!dbData) {

    const saveParams = {
      TableName: 'users',
      Key: {
        email: data.email,
        name: data.name,
        sub: data.sub, 
        last_activity: new Date().toISOString(),
        auth: "google"
      }
    };
    dynamodb.put(saveParams).promise();
  } else {
  
    const updateParams = {
      TableName: 'users',
      Key: {
        email: data.email
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

    console.log(data)
    return data;
  } catch (error: any) {
    console.log(error)
    return null;
  }
};
