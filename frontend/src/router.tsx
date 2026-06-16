import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from './ui/AppLayout';
import { DashboardPage } from './ui/DashboardPage';
import { NotFoundPage } from './ui/NotFoundPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      {
        index: true,
        element: <DashboardPage />,
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
]);
