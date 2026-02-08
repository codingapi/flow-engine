import { InfoCircleOutlined } from "@ant-design/icons";
import styled from 'styled-components';

// 使用样式组件 + 动态 props
interface BaseNodeStyleProps {
    primaryColor?: string;
}

export const BaseNodeStyle = styled.div<BaseNodeStyleProps>`
    align-items: flex-start;
    background-color: #fff;
    border: 1px solid rgba(6, 7, 9, 0.15);
    border-radius: 8px;
    box-shadow: 0 2px 6px 0 rgba(0, 0, 0, 0.04), 0 4px 12px 0 rgba(0, 0, 0, 0.02);
    display: flex;
    flex-direction: column;
    justify-content: center;
    position: relative;
    width: 260px;
    cursor: default;

    &.activated {
        border: 1px solid ${props => props.primaryColor || '#82a7fc'};
    }
`;

export const ErrorIcon = () => (
  <InfoCircleOutlined
    style={{
      position: 'absolute',
      color: 'red',
      left: -6,
      top: -6,
      zIndex: 1,
      background: 'white',
      borderRadius: 8,
    }}
  />
);
