import React from 'react';
import { Router, Switch, Route } from 'react-router-dom';
import { createBrowserHistory } from 'history';

import { Calendar } from './components/Calendar';
import { IssuePage } from './components/IssuePage';

const history = createBrowserHistory();

const App = () => {
  return (
    <Router history={history}>
      <Switch>
        <Route path="/" exact>
          <Calendar />
        </Route>
        <Route path={'/issues/:date'} component={IssuePage} />
      </Switch>
    </Router>
  );
};

export default App;
