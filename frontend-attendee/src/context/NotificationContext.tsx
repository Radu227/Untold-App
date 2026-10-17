import React, { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { Client } from '@stomp/stompjs';
import { LiveNotificationAlert } from '../components/LiveNotificationAlert';

interface NotificationContextType {
  lastUpdate: any | null;
}

const NotificationContext = createContext<NotificationContextType>({ lastUpdate: null });

export const useNotification = () => useContext(NotificationContext);

export const NotificationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [lastUpdate, setLastUpdate] = useState<any | null>(null);

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws/updates',
      debug: (str) => console.log('STOMP: ' + str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      console.log('Connected to WebSocket updates');
      client.subscribe('/topic/performances', (message) => {
        if (message.body) {
          const data = JSON.parse(message.body);
          console.log('Received performance update:', data);
          setLastUpdate(data);
        }
      });
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.activate();

    return () => {
      client.deactivate();
    };
  }, []);

  return (
    <NotificationContext.Provider value={{ lastUpdate }}>
      {children}
      {lastUpdate && <LiveNotificationAlert performance={lastUpdate} onClose={() => setLastUpdate(null)} />}
    </NotificationContext.Provider>
  );
};
