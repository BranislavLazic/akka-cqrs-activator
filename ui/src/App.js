import React from 'react';
import { Router, Switch, Route } from 'react-router-dom';
import { createBrowserHistory } from 'history';

import { Calendar } from './components/Calendar';
import { IssuePage } from './components/IssuePage';
import { SSEProvider } from 'react-hooks-sse';
import { baseUrl } from './components/IssuePage/issuesApi';
const history = createBrowserHistory();

const App = () => {
  return (
    <Router history={history}>
      <Switch>
        <Route path="/" exact>
          <Calendar />
        </Route>
        <Route path={'/issues/:date'}>
          <SSEProvider endpoint={`${baseUrl}/event-stream`}>
            <IssuePage />
          </SSEProvider>
        </Route>
      </Switch>
    </Router>
  );
};

export default App;
