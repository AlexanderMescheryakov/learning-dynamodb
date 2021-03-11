const fs = require('fs');
const mocker = require('mocker-data-generator').default
const stringify = require('csv-stringify')

const PRODUCTS_COUNT = 100000;
const CAT_COUNT = 100;
const CATS = Array.from(new Array(CAT_COUNT), (val, index)=> "CAT#Category" + index);

var productSchema = {
  Id: {
    faker: 'random.uuid'
  },
  PK: {
    function: function() {
        return 'PROD#' + this.object.Id;
    },
  },
  SK: {
      self: 'PK'
  },
  GSI1PK: {
    function: function() {
        return this.faker.random.arrayElement(CATS);
    },
  },
  GSI1SK: {
      faker: 'commerce.productName'
  },
  Data: {
    function: function() {
      return this.faker.commerce.price();
    }
  }
};

const data = mocker()
  .schema('products', productSchema, PRODUCTS_COUNT)
  .buildSync();

fs.writeFileSync('products.json', JSON.stringify(data.products));

var productsFileStream = fs.createWriteStream('products.csv');
stringify(data.products, {
  header: true, columns: [{ key: 'Id', header: 'productId' }]
}).pipe(productsFileStream);
