# Jupyterlab Timebase Clinent samples

## How to configure jupyterlab

Python 3.9+ is required

1) install jupyterlab from pip:
```
   pip install jupyterlab==3.4.8
```

3) install perspective python module:
```
   pip install perspective-python
```

5) install additional python modules if not installed (`ipywidgets`, `sortedcollections`)
```
   pip install ipywidgets
   pip install sortedcollections
```

7) install npm to be able to install jupyter extensions

8) install jupyterlab perspective extensions:
```
jupyter labextension install @finos/perspective-jupyterlab
jupyter labextension install @jupyter-widgets/jupyterlab-manager
```

9) start jupyter lab from this directory:
```
jupyter-lab
```

## How to build docker image

Run next command to build docker image:
```
docker build -t jupyter-test:1.0 .
```
