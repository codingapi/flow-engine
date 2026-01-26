import axios, {AxiosInstance} from "axios";
import {sleep} from "./sleep";
import {Base64Utils} from "./base64";

export interface MessageBox {
    success: (msg: string) => void;
    error: (msg: string) => void;
}

export type Response = {
    success: boolean;
    errCode?: string;
    errMessage?: string;
    data?: any;
    total?: number;
}

export class HttpClient {
    private readonly api: AxiosInstance;
    private readonly messageBox: MessageBox;

    constructor(timeout: number, messageBox: MessageBox, baseUrl = '') {
        this.messageBox = messageBox;
        this.api = axios.create({
            timeout: timeout,
            baseURL: baseUrl,
            headers: {
                "Content-Type": "application/json",
            },
        });

        this.addRequestInterceptors();
        this.addResponseInterceptors();
    }

    private addRequestInterceptors() {
        this.api.interceptors.request.use((config: any) => {
            const token = localStorage.getItem("token");
            if (token) {
                config.headers = {
                    Authorization: `${token}`,
                } as any;
            }
            return config;
        }, (error: any) => {
            return Promise.reject(error);
        });
    }

    private addResponseInterceptors() {
        this.api.interceptors.response.use(async (response: any) => {
                const headers = response.headers;
                const token = headers['authorization'];

                const state = response.status;
                if (state === 200) {
                    if (token) {
                        console.log('reset token', token);
                        localStorage.setItem("token", token)
                    }

                    if (response.data) {
                        const success = response.data.success;
                        if (!success) {
                            const errMessage = response.data.errMessage;
                            const errCode = response.data.errCode;
                            if ("token.expire" === errCode || "token.error" === errCode) {
                                this.messageBox.error('登录已过期，请退出再重新打开');
                                await sleep(1500);
                                localStorage.clear();
                                window.location.href = '/#login';
                            } else {
                                if ("login.error" === errCode) {
                                    return response;
                                }
                                this.messageBox.error(errMessage)
                            }
                        }
                    } else {
                        this.messageBox.error('抱歉，该账户无权限访问');
                    }
                }
                return response;
            },
            (error: any) => {
                const response = error.response;
                const state = response.data.status;

                if (state === 403) {
                    this.messageBox.error('抱歉，该账户无权限访问');
                    return {
                        data: {
                            success: false,
                        }
                    }
                }
                return Promise.reject(error);
            }
        )
    }

    public get = async (url: string, params?: any): Promise<Response> => {
        try {
            const response = await this.api.get(url, {
                params
            });
            return response.data as Response;
        } catch (err:any) {
            return {
                success: false,
                errMessage:err.message,
                errCode:'http.error'
            }
        }
    }

    public put = async (url: string, data: any): Promise<Response> => {
        try {
            const response = await this.api.put(url, data);
            return response.data as Response;
        } catch (err:any) {
            return {
                success: false,
                errMessage:err.message,
                errCode:'http.error'
            }
        }
    }

    public delete = async (url: string, params?: any): Promise<Response> => {
        try {
            const response = await this.api.delete(url, {
                params
            });
            return response.data as Response;
        } catch (err:any) {
            return {
                success: false,
                errMessage:err.message,
                errCode:'http.error'
            }
        }
    }

    public post = async (url: string, data: any): Promise<Response> => {
        try {
            const response = await this.api.post(url, data);
            return response.data as Response;
        } catch (err:any) {
            return {
                success: false,
                errMessage:err.message,
                errCode:'http.error'
            }
        }
    }


    public page = async (url: string, params: any, sort: any, filter: any, match: {
        key: string,
        type: string
    }[]): Promise<Response> => {
        const base64Match = Base64Utils.stringToBase64(JSON.stringify(match));
        const base64Sort = Base64Utils.stringToBase64(JSON.stringify(sort));
        const base64Filter = Base64Utils.stringToBase64(JSON.stringify(filter));

        const response = await this.get(url, {
            ...params,
            sort: base64Sort,
            filter: base64Filter,
            params: base64Match,
        });

        if (response.success) {
            const list = response.data.total > 0 ? response.data.list : [];
            return {
                data: list,
                success: response.success,
                total: response.data.total
            };
        } else {
            return {
                data: [],
                success: response.success,
                total: 0
            }
        }
    }


    public download = async (url: string, filename?: string) => {
        try {
            const token = localStorage.getItem("token");
            const response = await axios.get(url, {
                responseType: 'blob',
                headers: {
                    'Authorization': token,
                }
            });
            const bytes = await response.data;
            const blob = new Blob([bytes]);
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = filename || 'result.csv';
            a.click();
        } catch (e) {
            console.log(e);
        }
    }

    public postDownload = async (url: string, data: any, filename?: string) => {
        try {
            const token = localStorage.getItem("token");
            const response = await axios.post(url, data, {
                responseType: 'blob',
                headers: {
                    'Authorization': token,
                }
            });
            const bytes = await response.data;
            const blob = new Blob([bytes]);
            const downloadUrl = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = filename || 'result.csv';
            a.click();
        } catch (e) {
            console.log(e);
        }
    }

}