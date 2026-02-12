import { httpClient } from ".";

export const detail = (id:string) => {
    return httpClient.get('/api/cmd/record/detail',{id});
}

