{{- define "chart.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "chart.labels" -}}
app.kubernetes.io/name: {{ include "chart.fullname" . }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "chart.matchLabels" -}}
app.kubernetes.io/name: {{ include "chart.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "chart.imagePullSecrets" -}}
{{- range .Values.global.imagePullSecrets }}
  {{- if eq (typeOf .) "map[string]interface {}" }} 
- {{ toYaml . | trim }}
  {{- else }}
- name: {{ . }}
  {{- end }}
{{- end }}
{{- end -}}

{{- define "replicaSourceServer" -}}
{{- if .Values.replicator.config.sourceTimeBaseUrl -}}
{{- .Values.replicator.config.sourceTimeBaseUrl -}}
{{- else -}}
{{ printf "dxtick://%s:8011" (include "chart.fullname" .) }}
{{- end -}}
{{- end -}}

{{- define "cleanExceptScripts" -}}
{{- range .Values.cleaner.exceptStreams -}}{{- printf "%s;" . -}}{{- end -}}
{{- end -}}