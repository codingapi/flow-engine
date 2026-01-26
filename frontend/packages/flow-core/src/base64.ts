export class Base64Utils{

    public static base64ToBlob = (base64: string, type: string) => {
        const binStr = atob(base64.split(',')[1]);
        const len = binStr.length;
        const arr = new Uint8Array(len);
        for (let i = 0; i < len; i++) {
            arr[i] = binStr.charCodeAt(i);
        }
        return new Blob([arr], {type: type});
    }

    public static base64ToString = (base64: string) => {
        if (base64.indexOf('base64') !== -1) {
            base64 = base64.split(',')[1];
        }
        return atob(base64);
    }


    public static stringToBase64 = (str: string) => {
        return btoa(str);
    }

}