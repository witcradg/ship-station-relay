# Ship Station Relay Automation

## Overview

This application bill be built with Spring Boot and is designed to be run as a REST service.
Data will be sent from the Snipcart service to the REST service. The REST controller will invoke a method on business service object that will use the Ship Station API to asynchronously (but sequentially) create the customer record* and then create the invoice record.

When Ship Station prints the shipping labels, a webhook will POST a link to SSA providing a URL that can be used to retreive information about the orders[?] and tracking numbers. The REST service will invoke a GET using the provided URL to obtain the data. That data will be marshalled according to AfterShip requirements and will be POSTED[?] to AfterShip.

On order Creation
Snipcart -> SSA -> Ship Station(client) 

On label printing (through ShipStation On Shipping Webhook)
Ship Station -> SSA (access url)
SSA GET -> ShipStation (data)
SSA -> AfterShip

***
### Useful Links
Code
<p><a>https://www.concretepage.com/spring-5/spring-resttemplate-postforobject</a></p>
<p><a>https://www.baeldung.com/spring-resttemplate-post-json</a></p>

