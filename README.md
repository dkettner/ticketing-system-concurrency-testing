# ticketing-system-concurrency-testing

This was a project for researching the effectiveness of different kinds of tests for finding race conditions in moduliths.
The following modulith was used as the SUT: https://github.com/dkettner/ticketing-system

The main branch represents the state of the modulith before the start of the project. All other branches follow a different approach by adding certain tests. Depending on the approach the already existing tests of the main branch were reused or modified.

1. iteration_1: unit tests
1. iteration_2: integration tests
1. iteration_3: statical code analysis
1. iteration_4: manipulation of the order of events
1. iteration_5: load tests

This repository will receive no further updates because the project has been completed.
