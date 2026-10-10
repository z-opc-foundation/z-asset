import {Card, Space, Typography} from 'antd'
import {AppLayout} from '../../../../_shared/z-frontend-common-local/dist/z-frontend-common.es.js'
import Status from './Status'

const {Title, Paragraph} = Typography

export default function App() {
    return (
        <AppLayout
            menuItems={[
                {key: '/', label: '服务状态'},
            ]}
            appTitle="asset 服务台"
            appShort="asset-"
         appIcon={{icon: <img src="/icon.png" alt="ASSET" style={{width: "100%", height: "100%", objectFit: "cover", borderRadius: 8}}/>, color: '#64748b', label: 'ASSET'}}>
            <Space direction="vertical" size="large" style={{width: '100%'}}>
                <Card>
                    <Title level={3} style={{margin: 0}}>asset 服务台</Title>
                    <Paragraph type="secondary" style={{marginBottom: 0}}>
                        独立运行壳（lead 005 §9.1 suit）· 后端 actuator 探针见下方
                    </Paragraph>
                </Card>
                <Status/>
            </Space>
        </AppLayout>
    )
}
