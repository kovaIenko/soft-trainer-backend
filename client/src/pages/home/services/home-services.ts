import axios from "axios"

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
	const { data } = await axios.get(`http://ec2-3-101-132-173.us-west-1.compute.amazonaws.com:3001/api/google/userData?accessToken=${accessToken}`, {
		headers: {
			"Content-Type": "application/json",
			"Access-Control-Allow-Origin": "*"
		},
	})
	console.log(accessToken);
	return data
}
