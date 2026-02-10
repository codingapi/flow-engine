import {afterEach, describe, expect, test} from '@rstest/core';
import {cleanup} from '@testing-library/react';

describe.sequential('Demo', () => {

    afterEach(() => {
        // 清理每一次测试产生的数据
        cleanup();
    });

    test('add test', () => {
        const value = 1 + 100;
        expect(value).toEqual(101);
    });
});