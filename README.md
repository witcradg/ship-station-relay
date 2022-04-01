# Shopify-Square-Automation

## Overview

This application bill be built with Spring Boot and is designed to be run as a REST service.
Data will be sent from the Shopify application to the REST service using a PUT call (not POST).
The REST controller will invoke a method on business service object that will use the Square API to asynchronously (but sequentially) create the customer record* and then create the invoice record.
Once that is done, an SMS service will be invoked to send the customer a message with a link to the invoice.

Shopify -> SSA** -> Square(client)
				-> Square(invoice)
				-> SMS with link to invoice

*What happens if the customer record already exists when he places a second order later?

**Our application: Shopify-Square-Automation

*** 

The REST controller will receive the JSON object as a string from the body of the PUT request.

It will marshal the data into a POJO (possibly an Entity object) passing that object to a service class instance.

The service instance will use a WebClient instance to execute two HTTP Requests. Since the WebClient is designed to invoke API calls asynchronously care will need to be taken to make sure each API call is complete before executing the next call. 

Methods in the WebClient will make two HTTP requests to the Square API. 
The first will create the Customer record.
The second will create the invoice.

Once these two methods are complete a POST request is made to an SMS service like Twilio (there are others). This will require an account. (There may be other solutions, but I haven't researched them.)

***
### Useful Links
Code
<p><a>https://www.concretepage.com/spring-5/spring-resttemplate-postforobject</a></p>
<p><a>https://www.baeldung.com/spring-resttemplate-post-json</a></p>

Square reference

<p><a>https://developer.squareup.com/docs/customers-api/use-the-api/keep-records</a></p>
<p><a>https://developer.squareup.com/docs/customers-api/use-the-api/customer-webhooks</a></p>

### API WIP

customer
<p><a>https://developer.squareup.com/reference/square/customers-api/create-customer#example-code</a></p>

invoice
<p><a></a></p>

order
<p><a></a></p>

category
<p><a></a></p>

***
Notes
Questions for Devin
	credentials?
	
	Do you group customers within Square 
	(There are customer groups and customer segments APIs in addition to the Customers API or
	do you just use the Customers API?)
		
	on https://developer.squareup.com/docs/customers-api/use-the-api/get-started
	quote: To save customer contact information or cards on file, you must seek explicit permission from the customer. 
	
The CreateCustomer endpoint permits creating duplicate customer profiles. Before creating a new customer profile, you should call the SearchCustomers endpoint to search by phone number, email address, or reference ID to determine whether a profile for the customer already exists.



#### create customer

<p><a>https://developer.squareup.com/docs/customers-api/use-the-api/keep-records</a></p>
<code>
  {
  "customer": {
    "id": "GSA67K1YGCSQQ47KSW7J7WX53M",
    "created_at": "2020-05-27T01:06:18.682Z",
    "updated_at": "2020-05-27T01:06:18Z",
    "given_name": "John",
    "family_name": "Doe",
    "nickname": "Junior",
    "company_name": "ACME Inc.",
    "email_address": "john.doe.jr@acme.com",
    "phone_number": "+1 (206) 222-3456",
    "preferences": {
      "email_unsubscribed": false
    },
    "creation_source": "THIRD_PARTY",
    "version": 0
  }
}
</code>

#### create invoice
<p><a>https://developer.squareup.com/docs/invoices-api/overview</a></p>
 