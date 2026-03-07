import React, {CSSProperties, lazy} from "react";

interface IconProps {
    type: string;
    style?: CSSProperties;
}

// 动态导入图标的函数
const loadIcon = (type: string) => {
    return lazy(() =>
        import("@ant-design/icons")
            .then(module => {
                //@ts-ignore
                const IconComponent = module[type];
                if (!IconComponent) {
                    throw new Error(`Icon ${type} not found`);
                }
                return { default: IconComponent };
            })
    );
};

export const Icon: React.FC<IconProps> = ({ type, style }) => {
    const [IconComponent, setIconComponent] = React.useState<React.ComponentType<any> | null>(null);
    const [error, setError] = React.useState(false);

    React.useEffect(() => {
        let mounted = true;
        setError(false);

        // 动态导入图标
        import("@ant-design/icons")
            .then(module => {
                //@ts-ignore
                if (mounted && module[type]) {
                    //@ts-ignore
                    setIconComponent(() => module[type]);
                } else if (mounted) {
                    setError(true);
                }
            })
            .catch(() => {
                if (mounted) setError(true);
            });

        return () => {
            mounted = false;
        };
    }, [type]);

    if (error) {
        console.warn(`Icon ${type} not found`);
        return null;
    }

    if (!IconComponent) {
        return <div style={style}>Loading...</div>;
    }

    return <IconComponent style={style} />;
};