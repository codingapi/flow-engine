/**
 * Dispatch对象，对应React的useState的dispatch函数能力
 */
export type Dispatch<T> = (updater: ((prevState: T) => T) | T) => void;