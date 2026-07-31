# AWS S3 Integration Setup Guide

This guide explains how to set up Amazon S3 for storing dynamic media (such as customer review images and profile pictures) in the restaurant backend. The application uses a highly efficient **Presigned URL** architecture, meaning the frontend uploads files directly to S3 without passing binary data through the backend.

## 1. AWS Account & Bucket Setup
1. Log in to the [AWS Management Console](https://console.aws.amazon.com/).
2. Navigate to **S3** and click **Create bucket**.
3. Choose a globally unique bucket name (e.g., `bk-restaurant-media`) and your preferred AWS Region (e.g., `ap-south-1`).
4. Keep **Block all public access** enabled. The backend will generate secure, time-limited presigned URLs for both uploading and downloading images.
5. Create the bucket.

## 2. Configure Bucket CORS
Because the frontend (browser) will upload files directly to your S3 bucket via PUT requests, you must configure CORS (Cross-Origin Resource Sharing).

1. Go to your new S3 bucket.
2. Click the **Permissions** tab.
3. Scroll down to **Cross-origin resource sharing (CORS)** and click Edit.
4. Paste the following JSON (update the `AllowedOrigins` with your frontend URLs):

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["PUT", "GET"],
    "AllowedOrigins": [
      "http://localhost:5173",
      "https://your-production-domain.com"
    ],
    "ExposeHeaders": ["ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

## 3. Create IAM User and Credentials
The backend needs an IAM user with programmatic access to generate presigned URLs.

1. Navigate to **IAM** (Identity and Access Management) in AWS.
2. Go to **Users** > **Create user**.
3. Name the user (e.g., `restaurant-s3-uploader`).
4. Select **Attach policies directly** and click **Create policy**.
5. Paste the following JSON (replace `your-bucket-name` with your actual bucket name):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::your-bucket-name/*"
    }
  ]
}
```
6. Save the policy, attach it to the user, and finish creating the user.
7. Go to the new user's **Security credentials** tab and create an **Access Key**. Save the Access Key ID and Secret Access Key.

## 4. Environment Variables
Add the following to your backend `.env` file located in `resturarent-system/.env`:

```properties
AWS_S3_BUCKET_NAME=your-bucket-name
AWS_REGION=your-aws-region

# From your IAM User
AWS_ACCESS_KEY_ID=AKIA...
AWS_SECRET_ACCESS_KEY=wJalr...
```

The application will automatically bind these values using the `AwsS3Properties` configuration class and interact with AWS seamlessly.
