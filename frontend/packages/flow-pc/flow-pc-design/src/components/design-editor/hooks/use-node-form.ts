import { useContext } from 'react';
import { NodeFormContext } from '../context';

export function useNodeFormContext() {
    return useContext(NodeFormContext);
}
