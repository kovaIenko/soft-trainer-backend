import axios from 'axios';

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
    console.log(data)
    return data;
  } catch (error) {
    return null;
  }
};
