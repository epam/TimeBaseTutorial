# timebase

![Version: 0.6.1](https://img.shields.io/badge/Version-0.6.1-informational?style=flat-square) ![AppVersion: 6.1.1](https://img.shields.io/badge/AppVersion-6.1.1-informational?style=flat-square)

A Helm chart for Timebase server

**Homepage:** <https://timebase.info>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Aliaksei Banshchyk | <Aliaksei_Banshchyk@epam.com> |  |

## Source Code

* <https://github.com/epam/TimeBase-CE>
* <https://github.com/epam/TimebaseWS>
* <https://github.com/epam/TimeBaseTimescaleConnector>
* <https://github.com/epam/TimeBaseClickhouseConnector>
* <https://github.com/epam/TimeBaseKafkaConnector>

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| admin.enabled | bool | `true` |  |
| admin.env | object | `{}` |  |
| admin.headless | bool | `false` |  |
| admin.headless.annotations | object | `{}` |  |
| admin.headless.enabled | bool | `false` |  |
| admin.image | string | `"epam/timebase-ws-server:1.0.2"` |  |
| admin.imagePullPolicy | string | `"IfNotPresent"` |  |
| admin.ingress.annotations | object | `{}` |  |
| admin.ingress.enabled | bool | `false` |  |
| admin.ingress.extraPaths | list | `[]` |  |
| admin.ingress.hosts[0] | string | `"chart-example.local"` |  |
| admin.ingress.path | string | `"/*"` |  |
| admin.ingress.tls | list | `[]` |  |
| admin.logs.app | string | `"timebase-admin"` |  |
| admin.logs.logLevel | string | `"INFO"` |  |
| admin.logs.maxEntriesPerSecond | int | `300` |  |
| admin.podAnnotations | object | `{}` |  |
| admin.podLabels | object | `{}` | Additional pod labels |
| admin.resources | object | `{"limits":{"cpu":1,"memory":"1Gi"},"requests":{"cpu":0.2,"memory":"256Mi"}}` | Configure the resources for the timebase web admin |
| admin.secret.create | bool | `true` |  |
| admin.secret.records.SECURITY_OAUTH2_USERS_0_AUTHORITIES | string | `"TB_ALLOW_READ, TB_ALLOW_WRITE"` |  |
| admin.secret.records.SECURITY_OAUTH2_USERS_0_PASSWORD | string | `"$2y$10$B8iNWMVVfsFDw/HfmRfITue17k7yfnisO92Q93KcB31/qLWKw.XtS"` |  |
| admin.secret.records.SECURITY_OAUTH2_USERS_0_USERNAME | string | `"admin"` |  |
| affinity | object | `{}` |  |
| backupper.accesskey | string | `""` |  |
| backupper.bucketname | string | `""` |  |
| backupper.enabled | bool | `false` |  |
| backupper.image | string | `"finos/timebase-ce-client:6.1"` |  |
| backupper.imagePullPolicy | string | `"IfNotPresent"` |  |
| backupper.region | string | `""` |  |
| backupper.secretkey | string | `""` |  |
| backupper.streams[0] | string | `"accounts"` |  |
| backupper.streams[1] | string | `"deals"` |  |
| backupper.streams[2] | string | `"orders"` |  |
| backupper.streams[3] | string | `"transactions"` |  |
| backupper.streams[4] | string | `"users"` |  |
| cleaner.daystoKeep | int | `1` |  |
| cleaner.enabled | bool | `false` |  |
| cleaner.exceptStreams | list | `[]` |  |
| cleaner.image | string | `"finos/timebase-ce-client:6.1"` |  |
| cleaner.imagePullPolicy | string | `"IfNotPresent"` |  |
| cleaner.schedule | string | `"0 7 * * *"` |  |
| cleaner.streams | object | `{}` |  |
| fullnameOverride | string | `""` |  |
| global.imagePullSecrets | list | `[]` |  |
| global.logs.gelf | bool | `true` |  |
| global.logs.host | string | `"graylog.internal"` |  |
| global.logs.port | string | `"12201"` |  |
| global.secret.create | bool | `true` |  |
| loadBalancer.annotations | object | `{}` |  |
| loadBalancer.enabled | bool | `false` |  |
| loadBalancer.spec | object | `{}` |  |
| nameOverride | string | `""` |  |
| server.adminUsers | object | `{}` |  |
| server.debug | bool | `false` |  |
| server.gracefulShutdownTime | int | `60` |  |
| server.image | string | `"finos/timebase-ce-server:6.1"` |  |
| server.imagePullPolicy | string | `"IfNotPresent"` |  |
| server.initContainers | object | `{}` |  |
| server.logs.app | string | `"timebase-server"` |  |
| server.logs.gelf | bool | `true` |  |
| server.logs.host | string | `""` |  |
| server.logs.logLevel | string | `"INFO"` |  |
| server.logs.maxEntriesPerSecond | int | `300` |  |
| server.logs.port | string | `""` |  |
| server.maxEntriesPerSecond | int | `300` |  |
| server.oauth | object | `{}` |  |
| server.password | string | `""` |  |
| server.persistence.accessModes | string | `"ReadWriteOnce"` |  |
| server.persistence.size | string | `"10Gi"` |  |
| server.persistence.storageClass | string | `"gp2"` |  |
| server.podAnnotations | object | `{}` |  |
| server.podLabels | object | `{}` | Additional pod labels |
| server.properties.enableRemoteAccess | bool | `false` | Enable/disable Remote Access |
| server.properties.lingerInterval | string | `"5S"` | Connection Linger Time |
| server.properties.maxConnections | int | `100` | The maximum number of concurrent connections that the server will accept and process. |
| server.properties.readOnly | bool | `false` | Read-only mode |
| server.properties.safeMode | bool | `false` | Using Save Mode will provide additional logging and disable streams with errors on startup |
| server.readOnlyUsers | object | `{}` |  |
| server.resources | object | `{"limits":{"cpu":4,"memory":"6Gi"},"requests":{"cpu":2,"memory":"4Gi"}}` | Configure the resources for the timebase server |
| server.secret.create | bool | `true` |  |
| server.securityContext.fsGroup | int | `1801` |  |
| server.securityContext.runAsGroup | int | `1801` |  |
| server.securityContext.runAsUser | int | `1801` |  |
| server.serial | string | `""` |  |
| server.serviceMonitor.enabled | bool | `true` |  |
| server.serviceMonitor.interval | string | `"30s"` |  |
| server.serviceMonitor.labels.monitoring | string | `"application"` |  |
| server.serviceMonitor.namespace | string | `"monitoring"` |  |
| server.url | string | `""` | overwrite default url, must be like dxtick://user:password@timebase:8011 |
| server.user | string | `""` |  |
| server.version | float | `5` |  |
| storageClass.allowVolumeExpansion | bool | `true` |  |
| storageClass.create | bool | `true` | Set to true to creating the StorageClass automatically |
| storageClass.defaultClass | bool | `false` | Set StorageClass as the default StorageClass |
| storageClass.mountOptions | object | `{}` |  |
| storageClass.name | string | `"timebase"` | Set a StorageClass name |
| storageClass.parameters.type | string | `"gp2"` |  |
| storageClass.provisionerName | string | `"kubernetes.io/aws-ebs"` |  |
| storageClass.reclaimPolicy | string | `"Delete"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.10.0](https://github.com/norwoodj/helm-docs/releases/v1.10.0)
