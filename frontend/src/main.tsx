import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { MutationCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App as AntApp, ConfigProvider, theme } from 'antd'
import viVN from 'antd/locale/vi_VN'
import dayjs from 'dayjs'
import 'dayjs/locale/vi'
import { AuthProvider } from './features/auth/AuthContext'
import { AppRouter } from './router/AppRouter'
import './styles/app/base.css'
import './styles/app/layout.css'
import './styles/app/components.css'
import './styles/app/dashboard.css'
import './styles/app/login.css'
import './styles/app/responsive.css'

dayjs.locale('vi')

let queryClient: QueryClient

queryClient = new QueryClient({
  mutationCache: new MutationCache({
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notification-count'] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 20_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider
      locale={viVN}
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#47789f',
          colorInfo: '#47789f',
          colorSuccess: '#4b7968',
          colorWarning: '#896631',
          colorError: '#9c5050',
          colorBgLayout: '#f5f7f9',
          colorText: '#24313d',
          colorTextSecondary: '#536371',
          colorTextTertiary: '#71808c',
          colorBorder: '#dfe6eb',
          fontSize: 12,
          fontSizeSM: 11,
          fontSizeLG: 13,
          fontSizeHeading1: 22,
          fontSizeHeading2: 20,
          fontSizeHeading3: 15,
          fontSizeHeading4: 13,
          fontSizeHeading5: 13,
          fontWeightStrong: 600,
          lineHeight: 1.45,
          lineHeightHeading1: 1.25,
          lineHeightHeading2: 1.3,
          lineHeightHeading3: 1.4,
          lineHeightHeading4: 1.4,
          lineHeightHeading5: 1.4,
          borderRadius: 8,
          fontFamily: '"Segoe UI Variable Text", "Segoe UI Variable", "Segoe UI", ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, sans-serif',
        },
        components: {
          Layout: { bodyBg: '#f5f7f9', headerBg: '#ffffff', siderBg: '#21313c' },
          Card: { borderRadiusLG: 11, borderRadius: 8 },
          Button: {
            borderRadius: 8,
            controlHeight: 36,
            paddingInline: 14,
            fontWeight: 500,
            primaryColor: '#ffffff',
            defaultBg: '#ffffff',
            defaultBorderColor: '#cbd6de',
            colorPrimary: '#47789f',
            colorPrimaryHover: '#3e6c91',
            colorPrimaryActive: '#345c7b',
          },
          Input: { controlHeight: 36, borderRadius: 8, paddingInline: 10 },
          Select: { controlHeight: 36, borderRadius: 8 },
          Modal: { borderRadiusLG: 12 },
          Form: { itemMarginBottom: 16, labelFontSize: 12, labelColor: '#536371' },
          Typography: { titleMarginBottom: 0, titleMarginTop: 0 },
        },
      }}
    >
      <AntApp>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <AuthProvider>
              <AppRouter />
            </AuthProvider>
          </BrowserRouter>
        </QueryClientProvider>
      </AntApp>
    </ConfigProvider>
  </React.StrictMode>,
)
