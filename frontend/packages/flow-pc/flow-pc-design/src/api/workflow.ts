import { httpClient } from ".";

export const list = (request: any) => {
    return httpClient.page('/api/query/workflow/list', request, {}, {}, []);
}

export const options = () => {
    return httpClient.get('/api/query/workflow/options');
}

export const versions = (workId:string) => {
    return httpClient.get('/api/query/workflow/versions',{id:workId});
}

export const remove = (id:string) => {
    return httpClient.post('/api/cmd/workflow/remove',{id});
}

export const updateVersionName = (body:any) => {
    return httpClient.post('/api/cmd/workflow/updateVersionName',body);
}

export const changeVersion = (id:any) => {
    return httpClient.post('/api/cmd/workflow/changeVersion',{id});
}

export const changeState = (id:string) => {
    return httpClient.post('/api/cmd/workflow/changeState',{id});
}

export const create = () => {
    return httpClient.post('/api/cmd/workflow/create',{});
}

export const createNode = (type:string) => {
    return httpClient.post('/api/cmd/workflow/create-node',{type});
}

export const save = (body:any) => {
    return httpClient.post('/api/cmd/workflow/save',body);
}

export const load = (id:string) => {
    return httpClient.get('/api/cmd/workflow/load',{id});
}