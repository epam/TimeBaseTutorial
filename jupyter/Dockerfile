FROM ubuntu:20.04

ARG DEBIAN_FRONTEND=noninteractive

# install python
RUN apt update && apt -y install gcc make cmake g++ python3.9 python3-pip curl

# install nodejs
RUN curl -sL https://deb.nodesource.com/setup_14.x | bash && apt -y install nodejs

COPY . /demo
WORKDIR /demo

# install python dependencies
RUN pip3 install jupyterlab==3.4.8 perspective-python ipywidgets sortedcollections && \ 
    pip3 install timebase_client-6.0.1-py2.py3-none-any.whl

# install jupyter extensions
RUN jupyter labextension install @finos/perspective-jupyterlab && \ 
    jupyter labextension install @jupyter-widgets/jupyterlab-manager

EXPOSE 8888
ENTRYPOINT ["jupyter", "lab", "--ip=*", "--port=8888", "--NotebookApp.token=''", "--NotebookApp.password=''", "--no-browser", "--allow-root"]