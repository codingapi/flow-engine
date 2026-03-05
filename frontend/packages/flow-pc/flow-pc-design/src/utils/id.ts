import {nanoid} from "nanoid";

export class IdUtils{

    public static generateId(){
        return nanoid(6);
    }
}