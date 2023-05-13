import axios from "axios"

const SERVER_ENDPOINT_URL= import.meta.env.VITE_SERVER_URI;

//const SERVER_ENDPOINT_URL="http://localhost:3001";
export const getAccessTokenGithub = async (code: string): Promise<any> => {
	const { data } = await axios.get(`http://localhost:3001/api/github/accessToken?code=${code}`, {
		headers: {
			"Content-Type": "application/json",
		},
	})

	return data
}

export const getUserDataGithub = async (accessToken: string) => {
	const { data } = await axios.get(`http://localhost:3001/api/github/userData?accessToken=${accessToken}`, {
		headers: {
			"Content-Type": "application/json",
			"Access-Control-Allow-Origin": "*"
		},
	})
	return data
}

export const getUserDataGoogle = async (accessToken: string) => {
	console.log(accessToken);
	const { data } = await axios.get(`${SERVER_ENDPOINT_URL}/api/google/userData`, {
		headers: {
			"Content-Type": "application/json",
			"Access-Control-Allow-Origin": "*",
			"Authorization": `Bearer ${accessToken}`
		},
	})
	//console.log(accessToken);
	return data
}