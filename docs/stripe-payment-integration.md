# Stripe Payment Gateway Setup Guide

This guide explains how to set up the Stripe integration for the restaurant backend, which handles credit card payments and refunds using Stripe Payment Intents and Webhooks.

## 1. Prerequisites
- A [Stripe Developer Account](https://dashboard.stripe.com/register).
- The [Stripe CLI](https://stripe.com/docs/stripe-cli) installed on your local machine for webhook testing.

## 2. Obtain API Keys
1. Log in to your Stripe Dashboard.
2. Go to **Developers** > **API keys**.
3. Locate your **Secret key** (starts with `sk_test_...`). You will need this for the backend.
4. Locate your **Publishable key** (starts with `pk_test_...`). You will need this for the frontend environment variables.

## 3. Local Webhook Testing
Stripe uses webhooks to notify the backend asynchronously when a payment succeeds or a refund is processed. To test this locally, you must use the Stripe CLI to forward events to your localhost.

Run this command in your terminal:
```bash
stripe listen --forward-to localhost:8080/api/v1/webhooks/stripe
```
When you run this command, the CLI will output a **Webhook signing secret** (starts with `whsec_...`).

## 4. Environment Variables
Add the following to your backend `.env` file located in `resturarent-system/.env`:

```properties
# Your Secret Key from Step 2
STRIPE_API_KEY=sk_test_...

# Your Webhook Signing Secret from Step 3
STRIPE_WEBHOOK_SECRET=whsec_...
```

## 5. Production Deployment
When deploying to production:
1. In the Stripe Dashboard, go to **Developers** > **Webhooks**.
2. Click **Add an endpoint**.
3. Enter your production backend URL: `https://your-domain.com/api/v1/webhooks/stripe`.
4. Select the events to listen to (e.g., `payment_intent.succeeded`, `payment_intent.payment_failed`, `charge.refunded`).
5. Retrieve the new production Webhook Signing Secret and update your production environment variables.
