import { httpClient } from ".";

export const list = (request: any) => {
    console.log(request);
    return httpClient.page('/api/query/workflow/list', request, {}, {}, []);
}