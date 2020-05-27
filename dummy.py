# coding: utf8

from locust import User, TaskSet, task

class MyTaskSet(TaskSet):
    @task(20)
    def hello(self):
        pass

class Dummy(User):
    task_set = MyTaskSet
