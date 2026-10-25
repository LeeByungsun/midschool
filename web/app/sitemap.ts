import type { MetadataRoute } from "next";

const siteUrl = "https://midschool.vercel.app";

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: siteUrl, changeFrequency: "weekly" },
    { url: `${siteUrl}/android`, changeFrequency: "monthly" },
  ];
}
