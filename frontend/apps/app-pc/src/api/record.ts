import { httpClient } from "."

export const list = (request: any) => {
    return httpClient.page('/api/query/record/list', request, {}, {}, []);
}

export const todo = (request: any) => {
    return httpClient.page('/api/query/record/todo', request, {}, {}, []);
}

export const done = (request: any) => {
    return httpClient.page('/api/query/record/done', request, {}, {}, []);
}