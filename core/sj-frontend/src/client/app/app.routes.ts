import { AppRoutes } from './shared/models/routes.model';
import { ProvidersRoutes } from './providers/providers.routes';
import { ServicesRoutes } from './services/services.routes';
import { StreamsRoutes } from './streams/streams.routes';
import { ModulesRoutes } from './modules/modules.routes';
import { InstancesRoutes } from './instances/instances.routes';

export const routes: AppRoutes = [{
  path: '',
  breadcrumb: 'Stream Juggler',
  children: [
    { path: '', redirectTo: '/providers', pathMatch: 'full' },
    ...ProvidersRoutes,
    ...ServicesRoutes,
    ...StreamsRoutes,
    ...ModulesRoutes,
    ...InstancesRoutes,
  ]
}];
