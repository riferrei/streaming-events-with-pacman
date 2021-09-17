FROM golang:1.17

LABEL maintainer="Ricardo Ferreira <riferrei@riferrei.com>"

WORKDIR /redis-sink
COPY go.mod .
COPY go.sum .
COPY redis-sink.go .
RUN go build -o redis-sink .

CMD ["./redis-sink"]