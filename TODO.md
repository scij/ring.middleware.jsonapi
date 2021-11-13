# To Do

## Automate Link Generation
- `links/self` - can this be generated properly from request context?
- `relationships` pass collection of resources and generate references

## Handle request parameters
JSON:API supports a range of parameters to customize responses.
These can be implemented in a _to-be-implemented_ JSON:API request handler.
- `include` loads related objects along with the primary object
- `fields` to reduce the fields in the response.
- `sort` list sort fields.
- `page` pagination - details are implementation defined
- `filter` filtering of resources - also implementation defined

## Wiring
Handler delegates are registered by resource name
and contain
- a registry function that retrieves objects. Filter, sort and page need to be handled there.
- 
