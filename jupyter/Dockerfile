FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

# install python
RUN apt update && apt -y install gcc make cmake g++ python3.8 python3-pip curl

# install nodejs
RUN curl -sL https://deb.nodesource.com/setup_14.x | bash && apt -y install nodejs

COPY . /demo
WORKDIR /demo

# install python dependencies
RUN pip3 install -r requirements.txt && pip3 install timebase_client-6.0.1-py2.py3-none-any.whl # todo: install from pypi as soon module will be published

# install jupyter extensions
RUN jupyter labextension install @finos/perspective-jupyterlab@1.3.12 && jupyter labextension install @jupyter-widgets/jupyterlab-manager@3.1.0

EXPOSE 8888
ENTRYPOINT ["jupyter", "lab", "--ip=*", "--port=8888", "--NotebookApp.token=''", "--NotebookApp.password=''", "--no-browser", "--allow-root"]