import React from 'react';
import ReactDOM from 'react-dom/client';
import {RouterProvider} from 'react-router';
import {hashRouters} from './config/routers';
import {ConfigProvider} from "antd";
import zhCN from 'antd/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh');

const rootEl = document.getElementById('root');
if (rootEl) {
  const root = ReactDOM.createRoot(rootEl);
  root.render(
    <React.StrictMode>
        <ConfigProvider locale={zhCN}>
            <RouterProvider router={hashRouters}/>
        </ConfigProvider>
    </React.StrictMode>,
  );
}
